import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ContractReferenceMaterialLinksComponent } from './contract-reference-material-links.component';

describe('ContractReferenceMaterialLinksComponent', () => {
  let component: ContractReferenceMaterialLinksComponent;
  let fixture: ComponentFixture<ContractReferenceMaterialLinksComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ContractReferenceMaterialLinksComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ContractReferenceMaterialLinksComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
