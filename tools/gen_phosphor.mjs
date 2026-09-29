// Generates the Phosphor icon assets used by the app from the @phosphor-icons/core npm package.
//   npm pack @phosphor-icons/core@2.1.1 && tar -xzf phosphor-icons-core-2.1.1.tgz
//   node tools/gen_phosphor.mjs <path-to-extracted-package>
// Outputs:
//   app/src/main/assets/phosphor_fill.txt   one icon per line: name|category|tags|svgPathData (habit/reward icons)
//   app/src/main/res/drawable/ph_*.xml      VectorDrawables for the app's own UI icons
import fs from 'node:fs';
import path from 'node:path';
import { pathToFileURL } from 'node:url';

const pkg = process.argv[2];
if (!pkg) throw new Error('usage: node tools/gen_phosphor.mjs <package dir>');
const root = path.resolve(path.dirname(new URL(import.meta.url).pathname.replace(/^\/([A-Za-z]:)/, '$1')), '..');
const { icons } = await import(pathToFileURL(path.join(pkg, 'dist/index.mjs')).href);

const readPath = (weight, name) => {
  const file = path.join(pkg, 'assets', weight, weight === 'regular' ? `${name}.svg` : `${name}-${weight}.svg`);
  const svg = fs.readFileSync(file, 'utf8');
  return [...svg.matchAll(/<path d="([^"]+)"/g)].map((m) => m[1]).join(' ');
};

// Picker order: the categories a habit most likely comes from first.
const order = ['health & wellness', 'people', 'nature', 'games', 'media', 'objects', 'office', 'commerce',
  'finances', 'maps & travel', 'weather', 'communications', 'technology & development', 'design', 'editor',
  'system', 'arrows'];
const rank = (c) => { const i = order.indexOf(c); return i < 0 ? order.length : i; };
const lines = icons
  .filter((i) => !i.categories.includes('brands'))
  .map((i) => ({ i, cat: [...i.categories].sort((a, b) => rank(a) - rank(b))[0] }))
  .sort((a, b) => rank(a.cat) - rank(b.cat) || a.i.name.localeCompare(b.i.name))
  .map(({ i, cat }) => {
    const tags = i.tags.filter((t) => !t.startsWith('*')).join(' ').replace(/[|\n]/g, ' ');
    return `${i.name}|${cat}|${tags}|${readPath('fill', i.name)}`;
  });
const assets = path.join(root, 'app/src/main/assets');
fs.mkdirSync(assets, { recursive: true });
fs.writeFileSync(path.join(assets, 'phosphor_fill.txt'), lines.join('\n') + '\n');
console.log(`phosphor_fill.txt: ${lines.length} icons`);

// UI icons: [phosphor name, weight]
const ui = [
  ['house', 'regular'], ['house', 'fill'], ['list-checks', 'regular'], ['list-checks', 'fill'],
  ['gift', 'regular'], ['gift', 'fill'], ['chart-pie-slice', 'regular'], ['chart-pie-slice', 'fill'],
  ['gear-six', 'regular'], ['gear-six', 'fill'], ['check', 'bold'], ['x', 'bold'], ['plus', 'regular'],
  ['trash', 'regular'], ['caret-right', 'regular'], ['caret-left', 'regular'], ['caret-down', 'regular'],
  ['arrow-left', 'regular'], ['calendar-blank', 'regular'], ['magnifying-glass', 'regular'],
  ['translate', 'regular'], ['info', 'regular'], ['trophy', 'fill'], ['hourglass', 'regular'],
  ['circle-dashed', 'regular'], ['dots-six-vertical', 'regular'],
];
const drawable = path.join(root, 'app/src/main/res/drawable');
fs.mkdirSync(drawable, { recursive: true });
for (const [name, weight] of ui) {
  const res = `ph_${name.replace(/-/g, '_')}${weight === 'regular' ? '' : '_' + weight}`;
  const xml = `<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="256"
    android:viewportHeight="256">
    <path
        android:fillColor="#FF000000"
        android:pathData="${readPath(weight, name)}" />
</vector>
`;
  fs.writeFileSync(path.join(drawable, `${res}.xml`), xml);
}
console.log(`${ui.length} drawables`);
