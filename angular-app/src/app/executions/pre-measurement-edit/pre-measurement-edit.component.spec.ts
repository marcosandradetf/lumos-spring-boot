import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PreMeasurementEditComponent } from './pre-measurement-edit.component';

describe('PreMeasurementEditComponent', () => {
  let component: PreMeasurementEditComponent;
  let fixture: ComponentFixture<PreMeasurementEditComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PreMeasurementEditComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PreMeasurementEditComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
