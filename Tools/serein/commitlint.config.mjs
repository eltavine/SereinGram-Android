const printable = /^[\t\n\r\x20-\x7e]*$/;

export default {
  extends: ['@commitlint/config-conventional'],
  plugins: [
    {
      rules: {
        'serein-english': ({ header, body, footer }) => [
          [header, body, footer].every((part) => !part || printable.test(part)),
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
