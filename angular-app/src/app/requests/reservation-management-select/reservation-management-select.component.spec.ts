import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ReservationManagementSelectComponent } from './reservation-management-select.component';

describe('ReservationManagementSelectComponent', () => {
  let component: ReservationManagementSelectComponent;
  let fixture: ComponentFixture<ReservationManagementSelectComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReservationManagementSelectComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ReservationManagementSelectComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
