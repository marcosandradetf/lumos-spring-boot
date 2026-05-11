import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ButtonBackComponent } from './button-back.component';

describe('ButtonBackComponent', () => {
  let component: ButtonBackComponent;
  let fixture: ComponentFixture<ButtonBackComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ButtonBackComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ButtonBackComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
