const printable = /^[\t\n\r\x20-\x7e]*$/;
const trailer = /^[A-Za-z][A-Za-z-]*: \S/;

// Git trailers in the last paragraph, such as Signed-off-by, may carry names in any script.
const withoutTrailers = (text) => {
  const paragraphs = text.trimEnd().split(/\n\s*\n/);
  const last = paragraphs[paragraphs.length - 1].split('\n');
  return last.every((line) => trailer.test(line)) ? paragraphs.slice(0, -1).join('\n\n') : text;
};

export default {
  extends: ['@commitlint/config-conventional'],
  plugins: [
    {
      rules: {
        'serein-english': ({ header, body, footer }) => [
          [header, withoutTrailers([body, footer].filter(Boolean).join('\n\n'))].every((part) => !part || printable.test(part)),
          'commit messages must be written in English (printable ASCII only)',
        ],
      },
    },
  ],
  rules: {
    'header-max-length': [2, 'always', 72],
    'body-empty': [2, 'never'],
    'body-min-length': [2, 'always', 60],
    'body-max-line-length': [2, 'always', 100],
    'serein-english': [2, 'always'],
  },
};
