const fs = require('fs');
const { execSync } = require('child_process');

const inFile = process.argv[2];
const outFile = process.argv[3];

execSync(`npx --yes marked --input "${inFile}" --output "${outFile}.body"`, {stdio: 'inherit'});

let body = fs.readFileSync(outFile + '.body', 'utf8');

// Add id attributes to headings that match the TOC anchor format
body = body.replace(/<h([1-6])>(.*?)<\/h\1>/g, (match, level, text) => {
  const stripped = text.replace(/<[^>]*>/g, '');
  const id = stripped.toLowerCase()
    .replace(/[^\w\s-]/g, '')
    .replace(/\s+/g, '-')
    .replace(/-+/g, '-')
    .trim();
  return `<h${level} id="${id}">${text}</h${level}>`;
});

const html = `<!DOCTYPE html>
<html>
<head>
<meta charset='utf-8'>
<title>Hubitat HTTP Endpoints Wiki</title>
<style>
  body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; max-width: 960px; margin: 0 auto; padding: 20px; line-height: 1.6; color: #333; }
  table { border-collapse: collapse; width: 100%; margin: 1em 0; }
  th, td { border: 1px solid #ddd; padding: 8px 12px; text-align: left; }
  th { background: #f5f5f5; }
  tr:nth-child(even) { background: #fafafa; }
  code { background: #f0f0f0; padding: 2px 6px; border-radius: 3px; font-size: 0.9em; }
  pre { background: #f5f5f5; padding: 16px; border-radius: 6px; overflow-x: auto; }
  pre code { background: none; padding: 0; }
  blockquote { border-left: 4px solid #ddd; margin: 1em 0; padding: 0.5em 1em; color: #555; background: #f9f9f9; }
  h1 { border-bottom: 2px solid #333; padding-bottom: 10px; }
  h2 { border-bottom: 1px solid #ddd; padding-bottom: 8px; margin-top: 2em; }
  h2:target { background: #ffffcc; padding: 8px; }
  details { margin: 1em 0; }
  summary { cursor: pointer; font-weight: bold; }
  a { color: #0366d6; }
  hr { border: none; border-top: 1px solid #ddd; margin: 2em 0; }
  html { scroll-behavior: smooth; }
</style>
</head>
<body>
${body}
</body>
</html>`;
fs.writeFileSync(outFile, html);
fs.unlinkSync(outFile + '.body');
console.log('Written:', outFile, '(' + html.length + ' bytes)');
