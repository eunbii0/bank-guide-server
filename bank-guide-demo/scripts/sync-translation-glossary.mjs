import fs from 'node:fs';
import { fileURLToPath } from 'node:url';
import { tp, LANGUAGES } from '../src/i18n.js';

// i18n.js owns these app labels; the Java resource is a reproducible snapshot, never edited by hand.
const keys = ['products', 'transfer', 'deposit', 'savings', 'timeDeposit', 'earlyCancel',
  'cancelRequest', 'certificates', 'cards', 'join', 'cardNameCheck', 'cardNameCredit'];
const target = fileURLToPath(new URL('../../bank-guide-server/src/main/resources/translation-glossary.json', import.meta.url));
const text = JSON.stringify(keys.map((key) => ({ key, labels: Object.fromEntries(
  LANGUAGES.map(({ code }) => [code, tp(code, key)]),
)})), null, 2) + '\n';
if (process.argv.includes('--check')) {
  if (!fs.existsSync(target) || fs.readFileSync(target, 'utf8').replaceAll('\r\n', '\n') !== text) {
    console.error('App translation glossary is stale. Run npm run glossary:sync in bank-guide-demo.');
    process.exitCode = 1;
  } else console.log('App translation glossary matches src/i18n.js.');
} else {
  fs.writeFileSync(target, text, 'utf8');
  console.log('Updated the backend app translation glossary from src/i18n.js.');
}
