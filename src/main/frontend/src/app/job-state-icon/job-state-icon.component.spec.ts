import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { JobStateIconComponent } from './job-state-icon.component';

describe('JobStateIconComponent', () => {
  let component: JobStateIconComponent;
  let fixture: ComponentFixture<JobStateIconComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ JobStateIconComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(JobStateIconComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
