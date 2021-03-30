import { Component, Input, OnInit } from '@angular/core';
import { Job } from '../job';

@Component({
  selector: 'app-job-state-icon',
  templateUrl: './job-state-icon.component.html',
  styleUrls: ['./job-state-icon.component.css']
})
export class JobStateIconComponent implements OnInit {
  @Input() job: Job | undefined;

  constructor() { }

  ngOnInit() {
  }

}
