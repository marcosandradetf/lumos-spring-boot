import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PendingExecutionsComponent } from './pending-executions.component';

describe('PendingExecutionsComponent', () => {
  let component: PendingExecutionsComponent;
  let fixture: ComponentFixture<PendingExecutionsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PendingExecutionsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PendingExecutionsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
