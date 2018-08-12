import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'stateAlert'
})
export class StateAlertPipe implements PipeTransform {

  transform(value: any, args?: any): any {
    const state: String = value.toLowerCase();
    switch (state) {
      case 'waiting':
        return 'alert-info';
      case 'running':
        return 'alert-info';
      case 'success':
        return 'alert-success';
      case 'cancelled':
        return 'alert-warning';
      case 'systemerror':
        return 'alert-danger';
      case 'temporaryfailure':
        return 'alert-danger';
      case 'permanentfailure':
        return 'alert-danger';
    }
  }
}
