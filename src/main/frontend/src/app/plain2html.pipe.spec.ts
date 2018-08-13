import { Plain2htmlPipe } from './plain2html.pipe';

describe('Plain2htmlPipe', () => {
  it('create an instance', () => {
    const pipe = new Plain2htmlPipe();
    expect(pipe).toBeTruthy();
  });
  it('test string', () => {
    const value = 'This should turn into\n\n';
    const target = 'This&nbsp;should&nbsp;turn&nbsp;into<br /><br />';

    const pipe = new Plain2htmlPipe();
    expect(pipe.transform(value)).toBe(target);
  })
});
