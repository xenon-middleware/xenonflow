import { HttpClientModule } from '@angular/common/http';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { BrowserModule } from '@angular/platform-browser';
import { FaIconLibrary, FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { fas } from '@fortawesome/free-solid-svg-icons';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { AppComponent } from './app.component';
import { FilterPipe } from './filter.pipe';
import { JobDetailComponent } from './job-detail/job-detail.component';
import { JobListComponent } from './job-list/job-list.component';
import { JobStateIconComponent } from './job-state-icon/job-state-icon.component';
import { LoginComponent } from './login/login.component';
import { ModalContentComponent } from './modal-content/modal-content.component';
import { Plain2htmlPipe } from './plain2html.pipe';
import { ServerStatusComponent } from './server-status/server-status.component';
import { StateAlertPipe } from './state-alert.pipe';
import { StateNamePipe } from './state-name.pipe';
import { StatusFilterPipe } from './status-filter.pipe';
import { WINDOW_PROVIDERS } from './window.provider';

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
    Plain2htmlPipe,
    ServerStatusComponent,
    FilterPipe,
    StatusFilterPipe
  ],
  imports: [
    BrowserModule,
    HttpClientModule,
    FontAwesomeModule,
    FormsModule,
    NgbModule
  ],
  providers: [
    WINDOW_PROVIDERS
  ],
  bootstrap: [AppComponent],
  entryComponents: [ModalContentComponent]
})
export class AppModule {
    constructor(library: FaIconLibrary) {
        library.addIconPacks(fas);
    }
}
