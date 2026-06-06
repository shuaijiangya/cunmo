import { existsSync, readdirSync, readFileSync, statSync, writeFileSync } from 'node:fs';
import { dirname, posix, relative, resolve } from 'node:path';

const projectRoot = resolve('.');
const miniprogramRoot = resolve(projectRoot, 'dist/build/mp-weixin');
const rootConfigPath = resolve(projectRoot, 'project.config.json');
const distConfigPath = resolve(miniprogramRoot, 'project.config.json');
const stableBaseLibVersion = '3.8.12';

if (!existsSync(distConfigPath)) {
  console.warn('[mp-weixin] dist project.config.json not found, skip patch.');
  process.exit(0);
}

const rootConfig = existsSync(rootConfigPath)
  ? JSON.parse(readFileSync(rootConfigPath, 'utf8'))
  : {};
const distConfig = JSON.parse(readFileSync(distConfigPath, 'utf8'));
const appJsonPath = resolve(miniprogramRoot, 'app.json');

distConfig.appid = rootConfig.appid || distConfig.appid;
distConfig.projectname = rootConfig.projectname || distConfig.projectname || '存量魔方';
distConfig.libVersion = rootConfig.libVersion || stableBaseLibVersion;
distConfig.setting = {
  ...(distConfig.setting || {}),
  urlCheck: false,
  es6: true,
  postcss: true,
  minified: true,
  newFeature: false,
  compileHotReLoad: false,
  enhance: false,
  skylineRenderEnable: false,
};

writeFileSync(distConfigPath, `${JSON.stringify(distConfig, null, 2)}\n`);

if (existsSync(appJsonPath)) {
  const appJson = JSON.parse(readFileSync(appJsonPath, 'utf8'));
  appJson.lazyCodeLoading = 'requiredComponents';
  writeFileSync(appJsonPath, `${JSON.stringify(appJson, null, 2)}\n`);
}

const walkJsonFiles = (dir) => {
  const files = [];
  for (const name of readdirSync(dir)) {
    const file = resolve(dir, name);
    const stats = statSync(file);
    if (stats.isDirectory()) {
      files.push(...walkJsonFiles(file));
    } else if (name.endsWith('.json')) {
      files.push(file);
    }
  }
  return files;
};

const toRootComponentPath = (jsonFile, componentPath) => {
  if (!componentPath.startsWith('.')) {
    return componentPath;
  }

  const absolutePath = resolve(dirname(jsonFile), componentPath);
  const rootRelativePath = relative(miniprogramRoot, absolutePath).split(posix.sep).join('/');
  return `/${rootRelativePath}`;
};

for (const jsonFile of walkJsonFiles(miniprogramRoot)) {
  const json = JSON.parse(readFileSync(jsonFile, 'utf8'));
  if (!json.usingComponents || Object.keys(json.usingComponents).length === 0) {
    continue;
  }

  json.usingComponents = Object.fromEntries(
    Object.entries(json.usingComponents).map(([name, componentPath]) => [
      name,
      toRootComponentPath(jsonFile, componentPath),
    ]),
  );
  json.componentPlaceholder = {
    ...(json.componentPlaceholder || {}),
    ...Object.fromEntries(Object.keys(json.usingComponents).map((name) => [name, 'view'])),
  };
  writeFileSync(jsonFile, `${JSON.stringify(json, null, 2)}\n`);
}

console.log('[mp-weixin] patched dist config, lazyCodeLoading, and component placeholders.');
