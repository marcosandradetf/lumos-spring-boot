import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ImportPreMeasurementsComponent } from './import-pre-measurements.component';

describe('ImportPreMeasurementsComponent', () => {
  let component: ImportPreMeasurementsComponent;
  let fixture: ComponentFixture<ImportPreMeasurementsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ImportPreMeasurementsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ImportPreMeasurementsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
