import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MeasurementDetailsComponent } from './measurement-details.component';

describe('MeasurementDetailsComponent', () => {
  let component: MeasurementDetailsComponent;
  let fixture: ComponentFixture<MeasurementDetailsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MeasurementDetailsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MeasurementDetailsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
