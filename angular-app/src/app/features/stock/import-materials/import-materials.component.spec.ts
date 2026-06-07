import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ImportMaterialsComponent } from './import-materials.component';

describe('ImportMaterialsComponent', () => {
  let component: ImportMaterialsComponent;
  let fixture: ComponentFixture<ImportMaterialsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ImportMaterialsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ImportMaterialsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
