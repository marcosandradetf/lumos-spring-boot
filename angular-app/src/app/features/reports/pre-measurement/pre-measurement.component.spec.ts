import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PreMeasurementComponent } from './pre-measurement.component';

describe('PreMeasurementComponent', () => {
  let component: PreMeasurementComponent;
  let fixture: ComponentFixture<PreMeasurementComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PreMeasurementComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PreMeasurementComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
