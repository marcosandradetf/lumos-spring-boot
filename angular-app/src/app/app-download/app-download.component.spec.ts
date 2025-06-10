import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AppDownloadComponent } from './app-download.component';

describe('AppDownloadComponent', () => {
  let component: AppDownloadComponent;
  let fixture: ComponentFixture<AppDownloadComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppDownloadComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AppDownloadComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
