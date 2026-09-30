const mineflayer = require('mineflayer');
const { Vec3 } = require('vec3');

const [port, requestedVersion] = process.argv.slice(2);
// versions that share a protocol with the release minecraft-data names them after
const version = { '1.19.1': '1.19.2', '1.21.7': '1.21.8' }[requestedVersion] || requestedVersion;

if (!require('minecraft-data')(version)) {
  console.log(`E2E SKIP: the player library has no protocol data for ${requestedVersion}`);
  process.exit(2);
}
const failures = [];

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

function check(name, ok, detail) {
  console.log(`E2E ${ok ? 'PASS' : 'FAIL'}: ${name}${detail ? ` (${detail})` : ''}`);
  if (!ok) {
    failures.push(name);
  }
}

async function waitFor(probe, timeout) {
  const end = Date.now() + timeout;
  while (Date.now() < end) {
    const value = probe();
    if (value) {
      return value;
    }
    await sleep(250);
  }
  return null;
}

function connect(username) {
  const bot = mineflayer.createBot({
    host: '127.0.0.1',
    port: Number(port),
    username,
    version,
    auth: 'offline',
    checkTimeoutInterval: 60000,
  });
  bot.log = [];
  bot.on('messagestr', (message) => bot.log.push(message));
  bot.on('error', (error) => console.log(`${username} error: ${error.message}`));
  bot.on('kicked', (reason) => console.log(`${username} kicked: ${JSON.stringify(reason)}`));
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error(`${username} did not spawn`)), 90000);
    bot.once('spawn', () => {
      clearTimeout(timer);
      resolve(bot);
    });
  });
}

async function openWindow(bot, block) {
  let opened = false;
  const onOpen = () => {
    opened = true;
  };
  bot.once('windowOpen', onOpen);
  await bot.activateBlock(block).catch(() => {});
  await waitFor(() => opened, 4000);
  bot.removeListener('windowOpen', onOpen);
  if (opened && bot.currentWindow) {
    bot.closeWindow(bot.currentWindow);
    await sleep(300);
  }
  return opened;
}

async function main() {
  const owner = await connect('BPOwner');
  await sleep(3000);
  owner.chat('/gamemode creative');
  await sleep(1000);
  owner.chat('/fill ~-4 ~-1 ~-4 ~4 ~-1 ~4 stone');
  owner.chat('/fill ~-4 ~ ~-4 ~4 ~3 ~4 air');
  await sleep(1500);
  owner.chat('/give BPOwner chest 1');
  const item = await waitFor(() => owner.inventory.items().find((i) => i.name === 'chest'), 8000);
  check('owner receives a chest', Boolean(item));

  const reference = owner.blockAt(owner.entity.position.floored().offset(1, -1, 0));
  await owner.equip(item, 'hand');
  await owner.placeBlock(reference, new Vec3(0, 1, 0));
  await sleep(1500);
  const chestPosition = reference.position.offset(0, 1, 0);
  check('owner places a chest', owner.blockAt(chestPosition).name === 'chest');
  const notice = owner.log.find((line) => /protected/i.test(line));
  console.log(`placement notice: ${notice || 'none captured'}`);

  check('owner opens the protected chest', await openWindow(owner, owner.blockAt(chestPosition)));

  owner.chat('/bp');
  const answered = await waitFor(() => owner.currentWindow || owner.log.some((line) => /blockprot|bp /i.test(line)), 6000);
  check('/bp answers the owner', Boolean(answered));
  if (owner.currentWindow) {
    owner.closeWindow(owner.currentWindow);
    await sleep(300);
  }

  const intruder = await connect('BPIntruder');
  await sleep(2000);
  owner.chat('/tp BPIntruder BPOwner');
  await sleep(2500);
  const target = intruder.blockAt(chestPosition);
  check('intruder sees the chest', Boolean(target) && target.name === 'chest');
  check('intruder cannot open the protected chest', !(await openWindow(intruder, target)));

  await Promise.race([intruder.dig(target).catch(() => {}), sleep(15000)]);
  await sleep(1500);
  check('intruder cannot break the protected chest', owner.blockAt(chestPosition).name === 'chest');

  owner.quit();
  intruder.quit();
  await sleep(500);
}

main()
  .catch((error) => {
    console.log(`E2E FAIL: ${error.message}`);
    failures.push(error.message);
  })
  .finally(() => process.exit(failures.length ? 1 : 0));
