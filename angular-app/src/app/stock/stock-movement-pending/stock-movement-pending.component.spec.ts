import { ComponentFixture, TestBed } from '@angular/core/testing';

import { StockMovementPendingComponent } from './stock-movement-pending.component';

describe('StockMovementPendingComponent', () => {
  let component: StockMovementPendingComponent;
  let fixture: ComponentFixture<StockMovementPendingComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StockMovementPendingComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(StockMovementPendingComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
