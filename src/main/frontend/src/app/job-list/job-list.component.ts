import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { interval } from 'rxjs';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { JobService } from '../job.service';
import { Job } from '../job';

@Component({
  selector: 'app-job-list',
  templateUrl: './job-list.component.html',
  styleUrls: ['./job-list.component.css']
})
export class JobListComponent implements OnInit {

  jobs: Job[];

  constructor(
    private jobService: JobService,
    private modalService: NgbModal
  ) { }

  ngOnInit() {
    this.jobService.getAllJobs().subscribe(data => {
      // Read the result field from the JSON response.
      this.jobs = data;
    });
    interval(2500)
      .subscribe(() => {
        // Make the HTTP request:
        this.jobService.getAllJobs().subscribe(data => {
          // Read the result field from the JSON response.
          this.jobs = data;
        });
      });

    this.jobService.getUpdateListObserver.subscribe((value) => {
      if (value) {
        setTimeout(() => {
          this.jobService.getAllJobs().subscribe(data => {
            // Read the result field from the JSON response.
            this.jobs = data;
          });
          this.jobService.updateList = false;
        }, 2500);
      }
    });
  }

  onSelect(job: Job) {
    this.jobService.setSelectedJob = job;
  }

  deleteAllJobs(dialog): void {
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
