import { Component, OnInit } from '@angular/core';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { interval } from 'rxjs';
import { Job } from '../job';
import { JobService } from '../job.service';

@Component({
  selector: 'app-job-list',
  templateUrl: './job-list.component.html',
  styleUrls: ['./job-list.component.css']
})
export class JobListComponent implements OnInit {

  jobs: Job[];
  error: string | null;
  activeJobId: string | null;
  statusFilter: string | null;

  constructor(
    private jobService: JobService,
    private modalService: NgbModal
  ) {
    this.statusFilter = null;
    this.jobs = [];
    this.error =  null;
    this.activeJobId = null;
  }

  ngOnInit() {
    this.jobService.selectedJob.subscribe(x => {
      this.activeJobId = x ? x.id : null
    });
    this.getAllJobs();
    interval(2500)
      .subscribe(() => {
        this.getAllJobs();
      });

    this.jobService.getUpdateListObserver.subscribe((value) => {
      if (value) {
        setTimeout(() => {
          this.getAllJobs();
          this.jobService.updateList = false;
        }, 2500);
      }
    });
  }

  private getAllJobs() {
    this.jobService.getAllJobs().subscribe(data => {
      this.jobs = data;
      this.error = null;
    },
    error => {
      this.jobs = [];
      this.error = error.message;
    })
  }

  onSelect(job: Job) {
    this.jobService.setSelectedJob = job;
  }

  deleteAllJobs(dialog: any): void {
    this.modalService.open(dialog).result.then((result) => {
      this.jobs.forEach((job: Job) => {
        this.jobService.deleteJob(job.id).subscribe(
          (success) => {
            console.log('Job delete request sent for: ' + job.id);
          },
          (error) => {
            console.log('Error deleting job: ' + error);
          }
        );
      });
    }, (reason) => {
      // dismissed do nothing
    });
    this.jobService.updateList = true;
  }

}
