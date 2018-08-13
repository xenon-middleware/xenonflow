import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { HttpClientModule } from '@angular/common/http';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { FormsModule } from '@angular/forms';

import { AppComponent } from './app.component';
import { JobDetailComponent } from './job-detail/job-detail.component';
import { JobListComponent } from './job-list/job-list.component';
import { JobStateIconComponent } from './job-state-icon/job-state-icon.component';
import { StateAlertPipe } from './state-alert.pipe';
import { StateNamePipe } from './state-name.pipe';
import { WINDOW_PROVIDERS } from './window.provider';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { library } from '@fortawesome/fontawesome-svg-core';
import { faPause, faCog, faCheck, faBan, faTimes, faExclamationTriangle, faExternalLinkAlt } from '@fortawesome/free-solid-svg-icons';
import { LoginComponent } from './login/login.component';
import { ModalContentComponent } from './modal-content/modal-content.component';
import { Plain2htmlPipe } from './plain2html.pipe';

library.add(faPause, faCog, faCheck, faBan, faTimes, faExclamationTriangle, faExternalLinkAlt);

@NgModule({
  declarations: [
    AppComponent,
    JobDetailComponent,
    JobListComponent,
    JobStateIconComponent,
    StateAlertPipe,
    StateNamePipe,
    LoginComponent,
    ModalContentComponent,
    Plain2htmlPipe
  ],
  imports: [
    BrowserModule,
    HttpClientModule,
    FontAwesomeModule,
    FormsModule,
    NgbModule.forRoot()
  ],
  providers: [
    WINDOW_PROVIDERS
  ],
  bootstrap: [AppComponent],
  entryComponents: [ModalContentComponent]
})
export class AppModule { }
