import { Component, OnInit } from '@angular/core';
import { JobService } from './job.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css'],
  providers: [JobService]
})
export class AppComponent implements OnInit {
  panel: string;

  constructor(
    private jobService: JobService
  ) {
      this.panel = 'login';
   }

  ngOnInit() {
    this.jobService.isConnected.subscribe(connected => {
      if (connected) {
        this.panel = 'job-detail';
      } else {
        this.panel = 'login';
      }
    });

    this.jobService.selectedJob.subscribe((value) => {
      if (!this.jobService.isConnected.getValue()) {
        this.panel = 'login';
      } else {
        this.panel = 'job-detail';
      }
    });
  }
}
