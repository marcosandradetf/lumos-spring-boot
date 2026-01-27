import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AccountDrawerComponent } from './account-drawer.component';

describe('SidebarDrawerComponent', () => {
  let component: AccountDrawerComponent;
  let fixture: ComponentFixture<AccountDrawerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AccountDrawerComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AccountDrawerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
