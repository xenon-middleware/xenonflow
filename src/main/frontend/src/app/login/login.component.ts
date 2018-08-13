import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { JobService } from '../job.service';

export interface LoginModel {
  apiKeyName: string,
  apiKey: string
}

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit {
  model: LoginModel = {
    apiKeyName: '',
    apiKey: ''
  };

  status: string;
  
  constructor(
    private jobService: JobService
  ) {
    this.status = 'pending';
  }

  ngOnInit() {
  }

  onSubmit() {
    this.status = 'loading';
    this.jobService
      .connect(this.model.apiKeyName, this.model.apiKey)
      .then(_ => this.status = 'pending')
      .catch(_ => this.status = 'error');
  }
}