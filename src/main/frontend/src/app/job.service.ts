import { Injectable, Inject } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { Job } from './job';
import {HttpClient, HttpHeaders} from '@angular/common/http';
import { WINDOW } from './window.provider';

export interface WorkflowInput {
  [x: string]: object;
}

export interface JobDescription {
  name: string;
  input: WorkflowInput;
  workflow: string;
}

@Injectable({
  providedIn: 'root'
})
export class JobService {
  private _selectedJob: BehaviorSubject<Job>;
  private _updateList: BehaviorSubject<boolean>;
  private api;
  private headers: HttpHeaders;

  constructor(
    private http: HttpClient,
    @Inject(WINDOW) private window: Window
  ) {
    this._selectedJob = <BehaviorSubject<Job>>new BehaviorSubject(null);
    this._updateList = <BehaviorSubject<boolean>>new BehaviorSubject(false);
    this.headers = new HttpHeaders();
    this.headers = this.headers.set('api-key', 'in1uP28Y1Et9YGp95VLYzhm5Jgd$M!r0CKI7#@^RHwbVcHGa');

    this.api = this.window.location.origin + '/jobs';
  }

  get selectedJob() {
    return this._selectedJob.asObservable();
  }

  set setSelectedJob(job: Job) {
    this._selectedJob.next(job);
  }

  set updateList(update: boolean) {
    if (this._updateList.value !== update) {
      this._updateList.next(update);
    }
  }

  get getUpdateListObserver(): Observable<boolean> {
    return this._updateList.asObservable();
  }

  getJob(jobId: string): Observable<Job> {
    return this.http.get<Job>(this.api + '/' + jobId, { headers: this.headers });
  }

  getAllJobs(): Observable<Job[]> {
    return this.http.get<Job[]>(this.api, { headers: this.headers });
  }

  submitJob(jobDescription: JobDescription): Observable<Object> {
    return this.http.post(this.api, jobDescription, { headers: this.headers });
  }

  deleteJob(jobId: string): Observable<Object> {
    return this.http.delete(this.api + '/' + jobId, { headers: this.headers });
  }

  cancelJob(jobId: string): Observable<Object> {
    return this.http.post(this.api + '/' + jobId + '/cancel', null, { headers: this.headers });
  }
}