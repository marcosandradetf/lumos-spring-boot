import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ExecutionFinishedComponent } from './execution-finished.component';

describe('ExecutionFinishedComponent', () => {
  let component: ExecutionFinishedComponent;
  let fixture: ComponentFixture<ExecutionFinishedComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ExecutionFinishedComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ExecutionFinishedComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
