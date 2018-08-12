import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'stateName'
})
export class StateNamePipe implements PipeTransform {

  transform(value: any, args?: any): any {
    switch (value) {
      case 'Waiting':
        return value;
      case 'Running':
        return 'Processing';
      case 'Success':
        return value;
      case 'Cancelled':
        return value;
      case 'SystemError':
        return 'System Error';
      case 'TemporaryFailure':
        return 'Temporary Failure';
      case 'PermanentFailure':
        return 'Permanent Failure';
    }
  }

}
