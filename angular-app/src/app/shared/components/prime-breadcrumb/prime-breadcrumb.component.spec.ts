import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PrimeBreadcrumbComponent } from './prime-breadcrumb.component';

describe('PrimeBreadcrumbComponent', () => {
  let component: PrimeBreadcrumbComponent;
  let fixture: ComponentFixture<PrimeBreadcrumbComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PrimeBreadcrumbComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PrimeBreadcrumbComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
