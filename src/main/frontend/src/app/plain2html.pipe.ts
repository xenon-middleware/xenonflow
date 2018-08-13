import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'plain2html'
})
export class Plain2htmlPipe implements PipeTransform {

  transform(value: any, args?: any): any {
    let v:string = value.replace(/(?: )/g, '&nbsp;');
    return v.replace(/(?:\r\n|\r|\n)/g, '<br />');
  }

}
