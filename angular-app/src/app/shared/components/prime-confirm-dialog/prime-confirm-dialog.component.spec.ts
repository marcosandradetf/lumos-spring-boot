import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PrimeConfirmDialogComponent } from './prime-confirm-dialog.component';

describe('PrimeConfirmDialogComponent', () => {
  let component: PrimeConfirmDialogComponent;
  let fixture: ComponentFixture<PrimeConfirmDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PrimeConfirmDialogComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PrimeConfirmDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
