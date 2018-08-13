import { Injectable, Inject, isDevMode } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { Job } from './job';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { WINDOW } from './window.provider';

import { environment } from '../environments/environment';

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
  private _isConnected: BehaviorSubject<boolean>;
  private api;
  private headers: HttpHeaders;

  constructor(
    private http: HttpClient,
    @Inject(WINDOW) private window: Window
  ) {
    this._selectedJob = <BehaviorSubject<Job>>new BehaviorSubject(null);
    this._updateList = <BehaviorSubject<boolean>>new BehaviorSubject(false);
    this._isConnected = new BehaviorSubject<boolean>(false);
    if (isDevMode) {
      this.api = environment.api;
    } else {
      this.api = this.window.location.origin + '/jobs';
    }
  }

  connect(apiKeyHeaderName: string, apiKeyValue: string): Promise<void> {
    this.setApiKey(apiKeyHeaderName, apiKeyValue);
    return this.http.get(this.api, { headers: this.headers }).toPromise()
    .then(_ => {
      console.log('success!');
      this._isConnected.next(true);
    }).catch(error => {
      console.log('error!', error);
      this._isConnected.next(false);
      throw error;
    });
  }

  get isConnected(): BehaviorSubject<boolean> {
    return this._isConnected;
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

  setApiKey(apiKeyHeaderName: string, apiKeyValue: string) {
    this.headers = new HttpHeaders({
      [apiKeyHeaderName]: apiKeyValue
    });
  }

  getJob(jobId: string): Observable<Job> {
    return this.http.get<Job>(this.api + '/' + jobId, { headers: this.headers });
  }

  getJobLog(logUrl: string): Observable<string> {
    return this.http.get(logUrl, { headers: this.headers, responseType: 'text' });
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