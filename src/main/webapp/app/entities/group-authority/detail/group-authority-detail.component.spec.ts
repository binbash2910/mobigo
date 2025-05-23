import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of } from 'rxjs';

import { GroupAuthorityDetailComponent } from './group-authority-detail.component';

describe('GroupAuthority Management Detail Component', () => {
  let comp: GroupAuthorityDetailComponent;
  let fixture: ComponentFixture<GroupAuthorityDetailComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GroupAuthorityDetailComponent],
      providers: [
        provideRouter(
          [
            {
              path: '**',
              loadComponent: () => import('./group-authority-detail.component').then(m => m.GroupAuthorityDetailComponent),
              resolve: { groupAuthority: () => of({ id: 1264 }) },
            },
          ],
          withComponentInputBinding(),
        ),
      ],
    })
      .overrideTemplate(GroupAuthorityDetailComponent, '')
      .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(GroupAuthorityDetailComponent);
    comp = fixture.componentInstance;
  });

  describe('OnInit', () => {
    it('should load groupAuthority on init', async () => {
      const harness = await RouterTestingHarness.create();
      const instance = await harness.navigateByUrl('/', GroupAuthorityDetailComponent);

      // THEN
      expect(instance.groupAuthority()).toEqual(expect.objectContaining({ id: 1264 }));
    });
  });

  describe('PreviousState', () => {
    it('should navigate to previous state', () => {
      jest.spyOn(window.history, 'back');
      comp.previousState();
      expect(window.history.back).toHaveBeenCalled();
    });
  });
});
