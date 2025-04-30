import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PreMeasurementReportComponent } from './pre-measurement-report.component';

describe('PreMeasurementReportComponent', () => {
  let component: PreMeasurementReportComponent;
  let fixture: ComponentFixture<PreMeasurementReportComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PreMeasurementReportComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PreMeasurementReportComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
