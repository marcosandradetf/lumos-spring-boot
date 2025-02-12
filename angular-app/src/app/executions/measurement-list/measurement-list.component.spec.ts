import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MeasurementListComponent } from './measurement-list.component';

describe('MeasurementListComponent', () => {
  let component: MeasurementListComponent;
  let fixture: ComponentFixture<MeasurementListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MeasurementListComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MeasurementListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
