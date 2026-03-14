import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ExecutionNoWorkServiceComponent } from './execution-no-work-service.component';

describe('ExecutionNoWorkServiceComponent', () => {
  let component: ExecutionNoWorkServiceComponent;
  let fixture: ComponentFixture<ExecutionNoWorkServiceComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ExecutionNoWorkServiceComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ExecutionNoWorkServiceComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
