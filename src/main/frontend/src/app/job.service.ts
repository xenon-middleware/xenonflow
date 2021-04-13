import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { Job } from './job';
import { WINDOW } from './window.provider';


export interface WorkflowInput {
  [x: string]: object;
}

export interface JobDescription {
  name: string;
  input: WorkflowInput;
  workflow: string;
}

export interface ServerStatus {
  waiting: Number,
  running: Number,
  successful: Number,
  errored: Number
}

@Injectable({
  providedIn: 'root'
})
export class JobService {
  private _selectedJob: Subject<Job>;
  private _updateList: BehaviorSubject<boolean>;
  private _isConnected: BehaviorSubject<boolean>;
  private api: string;
  private statusUrl: string;
  private headers: HttpHeaders | undefined;

  constructor(
    private http: HttpClient,
    @Inject(WINDOW) private window: Window,
  ) {
    this._selectedJob = new Subject<Job>();
    this._selectedJob.next(undefined)
    this._updateList = new BehaviorSubject<boolean>(false);
    this._isConnected = new BehaviorSubject<boolean>(false);
    this.headers = undefined;
    let baseurl = this.window.location.origin + this.window.location.pathname.replace("/admin/index.html","")
    this.api =  baseurl + '/jobs';
    this.statusUrl = baseurl + '/status';
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

  set setSelectedJob(job: Job | undefined) {
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

  getUrl(url: string): Observable<string> {
    return this.http.get(url, { headers: this.headers, responseType: 'text' });
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

  getStatus(): Observable<ServerStatus> {
    return this.http.get<ServerStatus>(this.statusUrl, { headers: this.headers });
  }
}