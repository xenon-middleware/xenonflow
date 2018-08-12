import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { HttpClientModule } from '@angular/common/http';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';

import { AppComponent } from './app.component';
import { JobDetailComponent } from './job-detail/job-detail.component';
import { JobListComponent } from './job-list/job-list.component';
import { JobStateIconComponent } from './job-state-icon/job-state-icon.component';
import { StateAlertPipe } from './state-alert.pipe';
import { StateNamePipe } from './state-name.pipe';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { library } from '@fortawesome/fontawesome-svg-core';
import { faPause, faCog, faCheck, faBan, faTimes, faExclamationTriangle } from '@fortawesome/free-solid-svg-icons';

library.add(faPause, faCog, faCheck, faBan, faTimes, faExclamationTriangle);

@NgModule({
  declarations: [
    AppComponent,
    JobDetailComponent,
    JobListComponent,
    JobStateIconComponent,
    StateAlertPipe,
    StateNamePipe
  ],
  imports: [
    BrowserModule,
    HttpClientModule,
    FontAwesomeModule,
    NgbModule.forRoot()
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
