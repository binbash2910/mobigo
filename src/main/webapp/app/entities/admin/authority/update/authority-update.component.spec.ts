import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpResponse, provideHttpClient } from '@angular/common/http';
import { FormBuilder } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Subject, from, of } from 'rxjs';

import { AuthorityService } from '../service/authority.service';
import { IAuthority } from '../authority.model';
import { AuthorityFormService } from './authority-form.service';

import { AuthorityUpdateComponent } from './authority-update.component';

describe('Authority Management Update Component', () => {
  let comp: AuthorityUpdateComponent;
  let fixture: ComponentFixture<AuthorityUpdateComponent>;
  let activatedRoute: ActivatedRoute;
  let authorityFormService: AuthorityFormService;
  let authorityService: AuthorityService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [AuthorityUpdateComponent],
      providers: [
        provideHttpClient(),
        FormBuilder,
        {
          provide: ActivatedRoute,
          useValue: {
            params: from([{}]),
          },
        },
      ],
    })
      .overrideTemplate(AuthorityUpdateComponent, '')
      .compileComponents();

    fixture = TestBed.createComponent(AuthorityUpdateComponent);
    activatedRoute = TestBed.inject(ActivatedRoute);
    authorityFormService = TestBed.inject(AuthorityFormService);
    authorityService = TestBed.inject(AuthorityService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('should update editForm', () => {
      const authority: IAuthority = { name: 'c56c1cf7-aca8-48fe-ad81-eeebbf872cb1' };

      activatedRoute.data = of({ authority });
      comp.ngOnInit();

      expect(comp.authority).toEqual(authority);
    });
  });

  describe('save', () => {
    it('should call update service on save for existing entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IAuthority>>();
      const authority = { name: '572a7ecc-bf76-43f4-8026-46b42fba586d' };
      jest.spyOn(authorityFormService, 'getAuthority').mockReturnValue(authority);
      jest.spyOn(authorityService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ authority });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: authority }));
      saveSubject.complete();

      // THEN
      expect(authorityFormService.getAuthority).toHaveBeenCalled();
      expect(comp.previousState).toHaveBeenCalled();
      expect(authorityService.update).toHaveBeenCalledWith(expect.objectContaining(authority));
      expect(comp.isSaving).toEqual(false);
    });

    it('should call create service on save for new entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IAuthority>>();
      const authority = { name: '572a7ecc-bf76-43f4-8026-46b42fba586d' };
      jest.spyOn(authorityFormService, 'getAuthority').mockReturnValue({ name: null });
      jest.spyOn(authorityService, 'create').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ authority: null });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: authority }));
      saveSubject.complete();

      // THEN
      expect(authorityFormService.getAuthority).toHaveBeenCalled();
      expect(authorityService.create).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).toHaveBeenCalled();
    });

    it('should set isSaving to false on error', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IAuthority>>();
      const authority = { name: '572a7ecc-bf76-43f4-8026-46b42fba586d' };
      jest.spyOn(authorityService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ authority });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.error('This is an error!');

      // THEN
      expect(authorityService.update).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).not.toHaveBeenCalled();
    });
  });
});
