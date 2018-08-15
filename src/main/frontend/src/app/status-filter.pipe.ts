import { Pipe, PipeTransform } from '@angular/core';
import { Job } from './job';

@Pipe({
  name: 'statusFilter'
})
export class StatusFilterPipe implements PipeTransform {

  transform(items: Job[], status: string): any {
    if(!items) return [];
    if(!status) return items;
    status = status.toLowerCase();
    return items.filter((it: Job) => {
      return it.state.toLowerCase().includes(status);
    });
  }

}
