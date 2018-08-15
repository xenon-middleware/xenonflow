import { Component, OnInit } from '@angular/core';
import { JobService, ServerStatus } from '../job.service';
import { interval } from 'rxjs';

@Component({
  selector: 'app-server-status',
  templateUrl: './server-status.component.html',
  styleUrls: ['./server-status.component.css']
})
export class ServerStatusComponent implements OnInit {
  status: ServerStatus;

  constructor(
    private jobService: JobService
  ) { 
    this.status = null;
  }

  ngOnInit() {
    this.jobService.getStatus().subscribe(status => {
      this.status = status;
    }, error => {});
    interval(2500).subscribe(() => {
      this.jobService.getStatus().subscribe(status => {
        this.status = status;
      }, error => {});
    });
  }


}
