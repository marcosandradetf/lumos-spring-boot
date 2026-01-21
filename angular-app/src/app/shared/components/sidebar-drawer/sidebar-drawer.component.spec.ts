import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SidebarDrawerComponent } from './sidebar-drawer.component';

describe('SidebarDrawerComponent', () => {
  let component: SidebarDrawerComponent;
  let fixture: ComponentFixture<SidebarDrawerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SidebarDrawerComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SidebarDrawerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
