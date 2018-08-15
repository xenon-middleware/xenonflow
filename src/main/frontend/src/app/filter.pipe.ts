import { Pipe, PipeTransform } from '@angular/core';
import { Job } from './job';

@Pipe({
  name: 'filter'
})
export class FilterPipe implements PipeTransform {

  transform(items: Job[], searchText: string): Job[] {
    if(!items) return [];
    if(!searchText) return items;
    searchText = searchText.toLowerCase();
    return items.filter((it: Job) => {
      return it.name.toLowerCase().includes(searchText);
    });
  }

}
