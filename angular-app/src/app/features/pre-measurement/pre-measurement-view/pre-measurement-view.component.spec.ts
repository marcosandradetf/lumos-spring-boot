import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PreMeasurementViewComponent } from './pre-measurement-view.component';

describe('PreMeasurementViewComponent', () => {
  let component: PreMeasurementViewComponent;
  let fixture: ComponentFixture<PreMeasurementViewComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PreMeasurementViewComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PreMeasurementViewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
