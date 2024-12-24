import { ComponentFixture, TestBed } from '@angular/core/testing';

import { StockMovementApprovatedComponent } from './stock-movement-approvated.component';

describe('StockMovementApprovatedComponent', () => {
  let component: StockMovementApprovatedComponent;
  let fixture: ComponentFixture<StockMovementApprovatedComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StockMovementApprovatedComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(StockMovementApprovatedComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
