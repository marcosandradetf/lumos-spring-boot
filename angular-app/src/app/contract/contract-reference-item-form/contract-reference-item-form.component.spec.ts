import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ContractReferenceItemFormComponent } from './contract-reference-item-form.component';

describe('ContractReferenceItemFormComponent', () => {
  let component: ContractReferenceItemFormComponent;
  let fixture: ComponentFixture<ContractReferenceItemFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ContractReferenceItemFormComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ContractReferenceItemFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
