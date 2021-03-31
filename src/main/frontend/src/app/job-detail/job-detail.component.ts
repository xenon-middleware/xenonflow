import { Component, OnInit } from '@angular/core';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { interval } from 'rxjs';
import { Job } from '../job';
import { JobService } from '../job.service';
import { ModalContentComponent } from '../modal-content/modal-content.component';


@Component({
  selector: 'app-job-detail',
  templateUrl: './job-detail.component.html',
  styleUrls: ['./job-detail.component.css']
})
export class JobDetailComponent implements OnInit {
  job: Job | null = null;

  constructor(
    protected jobService: JobService,
    private modalService: NgbModal
  ) {
    interval(2500)
    .subscribe(() => {
      // Make the HTTP request:
      if (this.job != null) {
        this.jobService.getJob(this.job.id).subscribe((data) => {
          // Read the result field from the JSON response.
          this.job = data;
        },
        (error) => {
          console.log(error);
        });
      }
    });
  }

  ngOnInit() {
    this.jobService.selectedJob.subscribe((value: Job) => {
      this.job = value;
      if (this.job != null) {
        this.jobService.getJob(this.job.id).subscribe((data) => {
          this.job = data;
        },
        (error) => {
          console.log(error);
        });
      }
    });
  }

  keys(object: any): Array<string> {
    return Object.keys(object);
  }

  isErrorFile(key: string, object: any): boolean {
    return key === "stderr.txt" && this.isObject(object) && object.class === 'File' ;
  }

  isFile(object: any): boolean {
    return this.isObject(object) && object.class === 'File';
  }

  isObject(object: any): boolean {
    return (object !== null && typeof object === 'object' && !Array.isArray(object));
  }

  deleteJob(dialog: any): void {
    this.modalService.open(dialog).result.then((result) => {
      this.jobService.deleteJob(this.job!.id).subscribe(
        (success) => {
          console.log('Job delete request sent');
          this.jobService.setSelectedJob = undefined;
          this.jobService.updateList = true;
        },
        (error) => {
          console.log('Error deleting job: ' + error);
        }
      );
    }, (reason) => {
      // dismissed do nothing
    });
  }

  openLog(jobName: string, url: string) {
    this.jobService.getUrl(url).subscribe(content => {
      const modalRef = this.modalService.open(ModalContentComponent, { size: 'lg', windowClass: 'modal-xxl' });
      modalRef.componentInstance.title = jobName;
      modalRef.componentInstance.content = content;
    })
  }

  isCancelable(): boolean {
    if (this.job) {
      return this.job.state === 'Running' ||
             this.job.state === 'Waiting';
    } else {
      return false;
    }
  }

  cancelJob(dialog: any): void {
    this.modalService.open(dialog).result.then((result) => {
      this.jobService.cancelJob(this.job!.id).subscribe(
        (success) => {
          console.log('Job cancel request sent');
          this.jobService.updateList = true;
        },
        (error) => {
          console.log('Error cancelling job: ' + error);
        }
      );
    }, (reason) => {
      // dismissed do nothing
    });
  }
}
