import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GuideStateComponent } from './guide-state.component';

describe('GuideStateComponent', () => {
  let component: GuideStateComponent;
  let fixture: ComponentFixture<GuideStateComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GuideStateComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(GuideStateComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
